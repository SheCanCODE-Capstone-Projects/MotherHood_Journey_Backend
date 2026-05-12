package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;

public interface ChildService {

    /**
     * Register a new child. Creates the child entity and generates vaccination
     * records for every active (mandatory) EPI schedule.
     *
     * @param request the child registration details
     * @return the registered child response with a generated health ID
     * @throws com.motherhood.journey.common.exception.MotherNotFoundException
     *         if the mother ID in the request does not exist
     */
    ChildResponse registerChild(CreateChildRequest request);
}