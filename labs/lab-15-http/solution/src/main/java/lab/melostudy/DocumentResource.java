package lab.melostudy;

import java.net.URI;
import java.io.File;
import java.util.List;
import java.util.Set;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/** Documents, API version 1. */
@jakarta.ws.rs.Path("/api/v1/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/png", "text/plain");

    private final FileStorage storage;

    DocumentResource(FileStorage storage) {
        this.storage = storage;
    }

    /** The ETag of a document is its version, between quotes. Strong, because If-Match only accepts strong ones. */
    static EntityTag tagOf(Document document) {
        return new EntityTag(String.valueOf(document.version));
    }

    // ---- documents

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(DocumentInput input) {
        requireValid(input);
        Document document = new Document();
        document.title = input.title();
        document.content = input.content();
        document.persistAndFlush();
        return Response.created(URI.create("/api/v1/documents/" + document.id))
                .tag(tagOf(document)).entity(DocumentV1.of(document)).build();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}")
    public Response get(@RestPath long id, @jakarta.ws.rs.core.Context Request request) {
        Document document = Document.findById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        Response.ResponseBuilder notModified = request.evaluatePreconditions(tagOf(document));
        if (notModified != null) {
            return notModified.build(); // 304 when If-None-Match matches
        }
        return Response.ok(DocumentV1.of(document)).tag(tagOf(document)).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response replace(@RestPath long id, @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch,
            @jakarta.ws.rs.core.Context Request request, DocumentInput input) {
        requireValid(input);
        Document document = Document.findById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        if (ifMatch == null || ifMatch.isBlank()) {
            // The client did not say which version it is changing: refuse instead of risking a lost update
            return Response.status(428).entity("If-Match is required").type(MediaType.TEXT_PLAIN).build();
        }
        Response.ResponseBuilder failed = request.evaluatePreconditions(tagOf(document));
        if (failed != null) {
            return failed.build(); // 412: the document changed since the client read it
        }
        document.title = input.title();
        document.content = input.content();
        document.flush(); // the UPDATE ... WHERE version = ? runs here; a concurrent writer makes it fail (see ErrorMappers)
        return Response.ok(DocumentV1.of(document)).tag(tagOf(document)).build();
    }

    private static void requireValid(DocumentInput input) {
        if (input == null || input.title() == null || input.title().isBlank() || input.content() == null) {
            throw new BadRequestException("title and content are required");
        }
    }

    // ---- attachments

    @POST
    @jakarta.ws.rs.Path("/{id}/attachments")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public RestResponse<AttachmentInfo> upload(@RestPath long id, @RestForm("file") FileUpload file) {
        if (Document.findById(id) == null) {
            throw new NotFoundException();
        }
        if (file == null) {
            throw new BadRequestException("a part named 'file' is required");
        }
        String type = file.contentType() == null ? "" : file.contentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(type)) {
            return RestResponse.status(RestResponse.Status.UNSUPPORTED_MEDIA_TYPE);
        }
        Attachment attachment = new Attachment();
        attachment.documentId = id;
        attachment.fileName = plainFileName(file.fileName());
        attachment.contentType = type;
        attachment.size = file.size();
        attachment.storedName = storage.store(file.uploadedFile());
        attachment.persist();
        return RestResponse.ResponseBuilder
                .<AttachmentInfo>created(URI.create("/api/v1/documents/" + id + "/attachments/" + attachment.id))
                .entity(AttachmentInfo.of(attachment)).build();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}/attachments")
    public List<AttachmentInfo> attachments(@RestPath long id) {
        if (Document.findById(id) == null) {
            throw new NotFoundException();
        }
        return Attachment.of(id).stream().map(AttachmentInfo::of).toList();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}/attachments/{attachmentId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public RestResponse<File> download(@RestPath long id, @RestPath long attachmentId) {
        Attachment attachment = Attachment.findById(attachmentId);
        if (attachment == null || !attachment.documentId.equals(id)) {
            throw new NotFoundException();
        }
        return RestResponse.ResponseBuilder.ok(storage.locate(attachment.storedName).toFile())
                .header(HttpHeaders.CONTENT_TYPE, attachment.contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.fileName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    /** "..\\..\\evil.txt" and "../../evil.txt" both become "evil.txt"; characters that could break a header are replaced. */
    static String plainFileName(String clientName) {
        String name = clientName == null ? "" : clientName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").strip();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "file";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    public record AttachmentInfo(Long id, String fileName, String contentType, long size) {
        static AttachmentInfo of(Attachment attachment) {
            return new AttachmentInfo(attachment.id, attachment.fileName, attachment.contentType, attachment.size);
        }
    }
}
