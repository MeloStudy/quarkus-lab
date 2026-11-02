package lab.melostudy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Entity <-> DTO mapping for products, generated at COMPILE time (see target/generated-sources/annotations).
 * {@code unmappedTargetPolicy = ERROR}: a target property nobody maps is a build error, not a silent null.
 * Mappers read the entity, so call them where the Hibernate session is still open.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponses(List<Product> products);

    PriceChangeResponse toResponse(PriceChange change);

    List<PriceChangeResponse> toPriceChangeResponses(List<PriceChange> changes);

    /** The API shows labels as plain names, sorted. Reading the lazy collection here is what loads it. */
    default List<String> labelNames(Set<Label> labels) {
        return labels.stream().map(label -> label.name).sorted(Comparator.naturalOrder()).toList();
    }

    /** Everything the client does not control is ignored explicitly: the policy above makes it a decision. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "costPrice", ignore = true)
    @Mapping(target = "nameKey", ignore = true)
    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Product toNewEntity(ProductInput input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "costPrice", ignore = true)
    @Mapping(target = "nameKey", ignore = true)
    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    void updateEntity(ProductInput input, @MappingTarget Product product);
}
