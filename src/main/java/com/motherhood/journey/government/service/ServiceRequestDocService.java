package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.storage.FileStorageService;
import com.motherhood.journey.government.dto.response.ServiceRequestDocResponse;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.entity.ServiceRequestDoc;
import com.motherhood.journey.government.enums.DocumentType;
import com.motherhood.journey.government.repository.ServiceRequestDocRepository;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ServiceRequestDocService {

    private final ServiceRequestDocRepository docRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final FileStorageService fileStorageService;

    public ServiceRequestDocService(ServiceRequestDocRepository docRepository,
                                    ServiceRequestRepository serviceRequestRepository,
                                    FileStorageService fileStorageService) {
        this.docRepository = docRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.fileStorageService = fileStorageService;
    }

    public ServiceRequestDocResponse attach(UUID requestId, DocumentType documentType, MultipartFile file) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
            .orElseThrow(() -> new CustomException("Service request not found", HttpStatus.NOT_FOUND));

        FileStorageService.StoredFile stored =
            fileStorageService.store(file, "service-requests/" + requestId);

        ServiceRequestDoc doc = ServiceRequestDoc.builder()
            .request(sr)
            .documentType(documentType.name())
            .filePath(stored.path())
            .fileHash(stored.sha256())
            .build();

        return ServiceRequestDocResponse.from(docRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDocResponse> getByRequest(UUID requestId) {
        return docRepository.findByRequest_Id(requestId)
            .stream().map(ServiceRequestDocResponse::from).toList();
    }
}
