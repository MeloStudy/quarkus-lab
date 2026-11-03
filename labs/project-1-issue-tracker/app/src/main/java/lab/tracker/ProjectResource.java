package lab.tracker;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;

/** Projects, and the one thing that belongs to a project's URL: opening an issue in it. */
@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    private final ProjectService projects;
    private final IssueService issues;

    ProjectResource(ProjectService projects, IssueService issues) {
        this.projects = projects;
        this.issues = issues;
    }

    @POST
    public RestResponse<ProjectResponse> create(@NotNull @Valid ProjectRequest request) {
        ProjectResponse created = projects.create(request);
        return RestResponse.ResponseBuilder.<ProjectResponse>created(URI.create("/api/projects/" + created.code()))
                .entity(created).build();
    }

    @GET
    public List<ProjectResponse> list() {
        return projects.list();
    }

    @GET
    @Path("/{code}")
    public ProjectResponse get(@RestPath String code) {
        return projects.get(code);
    }

    @GET
    @Path("/{code}/summary")
    public SummaryResponse summary(@RestPath String code) {
        return projects.summary(code);
    }

    @POST
    @Path("/{code}/issues")
    public RestResponse<IssueResponse> open(@RestPath String code, @NotNull @Valid CreateIssueRequest request) {
        IssueResponse created = issues.create(code, request);
        return RestResponse.ResponseBuilder.<IssueResponse>created(URI.create("/api/issues/" + created.key()))
                .entity(created)
                .tag(new EntityTag(Long.toString(created.version())))
                .build();
    }
}
