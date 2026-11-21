package tn.esprit.rescueops.user.mapper;

import org.mapstruct.Mapper;
import tn.esprit.rescueops.user.domain.UserProfile;
import tn.esprit.rescueops.user.dto.UserProfileResponse;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    UserProfileResponse toResponse(UserProfile profile);
}
