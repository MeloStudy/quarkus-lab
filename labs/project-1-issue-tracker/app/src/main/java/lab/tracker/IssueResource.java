package lab.tracker;

import java.util.List;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * Issues, addressed by their key ({@code /api/issues/TRK-12}). The ETag of an issue is its version: GET gives it,
 * PUT and transitions must send it back in {@code If-Match} (lab 15).
 */
@Path("/api/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IssueResource {

    private final IssueService issues;

    IssueResource(IssueService issues) {
        this.issues = issues;
    }

    /**
     * Enums and numbers arrive as validated TEXT on purpose. JAX-RS answers 404 when a query parameter cannot be
     * converted ({@code ?status=DONE}, {@code ?page=abc}): a list route that exists would say "not found" for a
     * client mistake. With a pattern, the same mistake is a 400 that names the parameter.
     */
    @GET
    public RestResponse<List<IssueResponse>> list(
            @RestQuery @Pattern(regexp = "[A-Z]{2,5}") String project,
            @RestQuery @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED") String status,
            @RestQuery @Size(min = 1, max = 40) String assignee,
            @RestQuery @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL") String priority,
            @RestQuery @Pattern(regexp = "[a-z0-9-]{1,20}") String label,
            @RestQuery @Size(min = 1, max = 100) String text,
            @RestQuery @DefaultValue("-created") @Pattern(regexp = "-?(created|updated|priority)") String sort,
            @RestQuery @DefaultValue("0") @Pattern(regexp = "[0-9]{1,6}") String page,
            @RestQuery @DefaultValue("20") @Pattern(regexp = "[1-9][0-9]?|100") String size) {
        IssueFilter filter = new IssueFilter(project, status == null ? null : Status.valueOf(status), assignee,
                priority == null ? null : Priority.valueOf(priority), label, text);
        IssueService.IssueList result = issues.page(filter, sort, Integer.parseInt(page), Integer.parseInt(size));
        return RestResponse.ResponseBuilder.ok(result.items()).header("X-Total-Count", result.total()).build();
    }

    @GET
    @Path("/{key}")
    public Response get(@RestPath String key, @Context Request request) {
        IssueResponse issue = issues.get(key);
        EntityTag tag = tagOf(issue);
        // If-None-Match with the current ETag: nothing to send, 304 (the client already has this version)
        Response.ResponseBuilder notModified = request.evaluatePreconditions(tag);
        if (notModified != null) {
            return notModified.tag(tag).build();
        }
        return Response.ok(issue).tag(tag).build();
    }

    @PUT
    @Path("/{key}")
    public Response update(@RestPath String key, @RestHeader("If-Match") String ifMatch,
            @NotNull @Valid UpdateIssueRequest request) {
        IssueResponse updated = issues.update(key, versionOf(ifMatch), request);
        return Response.ok(updated).tag(tagOf(updated)).build();
    }

    @POST
    @Path("/{key}/transitions")
    public Response transition(@RestPath String key, @RestHeader("If-Match") String ifMatch,
            @NotNull @Valid TransitionRequest request) {
        IssueResponse moved = issues.transition(key, versionOf(ifMatch), request.to());
        return Response.ok(moved).tag(tagOf(moved)).build();
    }

    @POST
    @Path("/{key}/comments")
    public RestResponse<CommentResponse> comment(@RestPath String key, @NotNull @Valid CommentRequest request) {
        return RestResponse.status(RestResponse.Status.CREATED, issues.addComment(key, request));
    }

    @GET
    @Path("/{key}/comments")
    public List<CommentResponse> comments(@RestPath String key) {
        return issues.comments(key);
    }

    @GET
    @Path("/{key}/history")
    public List<EventResponse> history(@RestPath String key) {
        return issues.history(key);
    }

    private static EntityTag tagOf(IssueResponse issue) {
        return new EntityTag(Long.toString(issue.version()));
    }

    /**
     * {@code If-Match: "3"} -> 3. No header -> null (the service answers 428). A value that is not one of our ETags
     * (garbage, {@code *}, a weak tag) can never match: -1 is no version, so the service answers 412.
     */
    private static Long versionOf(String ifMatch) {
        if (ifMatch == null) {
            return null;
        }
        String value = ifMatch.strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
