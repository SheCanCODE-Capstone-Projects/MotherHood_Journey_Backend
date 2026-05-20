package com.motherhood.journey.child.mapper;

import com.motherhood.journey.child.dto.response.ChildSummaryDTO;
import com.motherhood.journey.child.entity.Child;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChildMapper {

    @Mapping(source = "mother.id", target = "motherId")
    ChildSummaryDTO toSummary(Child child);
}
