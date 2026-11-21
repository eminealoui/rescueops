package tn.esprit.rescueops.hospital.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.esprit.rescueops.hospital.domain.Hopital;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.dto.HopitalResponse;
import tn.esprit.rescueops.hospital.dto.ServiceResponse;

@Mapper(componentModel = "spring")
public interface HospitalMapper {

    HopitalResponse toResponse(Hopital hopital);

    @Mapping(target = "hopitalId", source = "hopital.id")
    ServiceResponse toResponse(ServiceHospitalier service);
}
