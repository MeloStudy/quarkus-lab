package lab.melostudy;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;

/** Maps the review hierarchy: each concrete entity to its own response record. */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR,
        subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface ReviewMapper {

    @SubclassMapping(source = CustomerReview.class, target = ReviewResponse.Customer.class)
    @SubclassMapping(source = CriticReview.class, target = ReviewResponse.Critic.class)
    ReviewResponse toResponse(Review review);

    List<ReviewResponse> toResponses(List<Review> reviews);
}
