package com.machines.machines_api.models.dto.response;

import com.machines.machines_api.models.dto.common.CategoryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CategoryResponseDTO extends CategoryDTO {
    protected List<SubcategoryResponseDTO> subcategories;
    private UUID id;

    public void setSubcategories(List<SubcategoryResponseDTO> subcategories) {
        this.subcategories = subcategories.stream().filter(x -> x.getDeletedAt() == null).toList();
    }
}
