package com.machines.machines_api.models.dto.response;

import com.machines.machines_api.models.dto.common.RegionDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RegionResponseDTO extends RegionDTO {
    private UUID id;
    private UUID countryId;
    private List<CityResponseDTO> cities;
    private LocalDateTime deletedAt;

    public void setCities(List<CityResponseDTO> cities) {
        this.cities = cities.stream().filter(x -> x.getDeletedAt() == null).toList();
    }
}
