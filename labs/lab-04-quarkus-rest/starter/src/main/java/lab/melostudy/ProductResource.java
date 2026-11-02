package lab.melostudy;

import jakarta.ws.rs.Path;

/**
 * REST API for products, mounted at /products.
 *
 * TODO (Task 1): GET /products/{id}  -> 200 + JSON, or 404 if it does not exist
 * TODO (Task 2): POST /products      -> 201 + Location header + the created product
 * TODO (Task 3): PUT /products/{id}  -> 200 + updated product, or 404
 *                DELETE /products/{id} -> 204, or 404
 * TODO (Task 4): GET /products       -> list, with ?category=, ?page= (default 0), ?size= (default 10)
 *                and an X-Total-Count header holding the total BEFORE paging
 *
 * Inject ProductRepository through the constructor.
 */
@Path("/products")
public class ProductResource {
}
