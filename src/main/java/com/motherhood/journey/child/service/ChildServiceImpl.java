package com.motherhood.journey.child.service;

import com.motherhood.journey.child.repository.ChildRepository;
import org.springframework.stereotype.Service;

@Service
public class ChildServiceImpl implements ChildService {
    private final ChildRepository childRepository;

    public ChildServiceImpl(ChildRepository childRepository) {
        this.childRepository = childRepository;
    }
}
