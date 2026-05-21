package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.dto.request.AttachDocumentRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestDocResponse;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.entity.ServiceRequestDoc;
import com.motherhood.journey.government.repository.ServiceRequestDocRepository;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ServiceRequestDocService {

    private final ServiceRequestDocRepository docRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestDocService(ServiceRequestDocRepository docRepository,
                                    ServiceRequestRepository serviceRequestRepository) {
        this.docRepository = docRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    public ServiceRequestDocResponse attach(UUID requestId, AttachDocumentRequest request) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
            .orElseThrow(() -> new CustomException("Service request not found", HttpStatus.NOT_FOUND));

        ServiceRequestDoc doc = ServiceRequestDoc.builder()
            .request(sr)
            .documentType(request.documentType().name())
            .filePath(request.filePath())
            .fileHash(request.fileHash())
            .build();

        return ServiceRequestDocResponse.from(docRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDocResponse> getByRequest(UUID requestId) {
        return docRepository.findByRequest_Id(requestId)
            .stream().map(ServiceRequestDocResponse::from).toList();
    }
}
