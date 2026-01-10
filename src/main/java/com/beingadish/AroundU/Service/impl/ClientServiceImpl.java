package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Update.ClientUpdateRequestDTO;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Exceptions.Client.ClientAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Client.ClientNotFoundException;
import com.beingadish.AroundU.Exceptions.Client.ClientValidationException;
import com.beingadish.AroundU.Mappers.User.Client.ClientMapper;
import com.beingadish.AroundU.Models.ClientModel;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Client.ClientWriteRepository;
import com.beingadish.AroundU.Service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientMapper clientMapper;
    private final ClientReadRepository clientReadRepository;
    private final ClientWriteRepository clientWriteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerClient(ClientRegisterRequestDTO requestDTO) {
        // Convert the RequestDTO to ClientModel
        ClientModel clientModel = clientMapper.registerRequestDtoToModel(requestDTO);

        // Validating if Client does not already exist (email/phone)
        if (Boolean.TRUE.equals(clientReadRepository.existsByEmail(clientModel.getEmail()))) {
            throw new ClientAlreadyExistException("Client with the given email already exists.");
        }

        if (Boolean.TRUE.equals(clientReadRepository.existsByPhoneNumber(clientModel.getPhoneNumber()))) {
            throw new ClientAlreadyExistException("Client with the given phone number already exists.");
        }

        clientModel.setHashedPassword(passwordEncoder.encode(requestDTO.getPassword()));
        // Save the client entity in the database
        clientWriteRepository.save(clientMapper.modelToEntity(clientModel));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailsResponseDTO getClientDetails(Long clientId) {
        Optional<Client> clientOptional = clientReadRepository.findById(clientId);
        if (clientOptional.isPresent()) {
            ClientModel model = clientMapper.entityToModel(clientOptional.get());
            return clientMapper.modelToClientDetailsResponseDto(model);
        } else {
            throw new ClientNotFoundException("Client with id %d does not exists".formatted(clientId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDetailsResponseDTO> getAllClients(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Client> pageData = clientReadRepository.findAll(pageable);
        return pageData.map(clientMapper::entityToModel).map(clientMapper::modelToClientDetailsResponseDto);
    }

    @Override
    @Transactional
    public ClientDetailsResponseDTO updateClientDetails(Long clientId, ClientUpdateRequestDTO updateRequest) {

        Client foundClientEntity = clientReadRepository.findById(clientId).orElseThrow(() -> new ClientValidationException("Cannot update, client not found"));

        if (updateRequest.getName() != null) {
            foundClientEntity.setName(updateRequest.getName());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(foundClientEntity.getEmail())) {
            if (Boolean.TRUE.equals(clientReadRepository.existsByEmail(updateRequest.getEmail()))) {
                throw new ClientValidationException("Email already in use");
            }
            foundClientEntity.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPhoneNumber() != null) {
            foundClientEntity.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getProfileImageUrl() != null) {
            foundClientEntity.setProfileImageUrl(updateRequest.getProfileImageUrl());
        }

        Client updatedClientEntity = clientWriteRepository.save(foundClientEntity);
        ClientModel updatedModel = clientMapper.entityToModel(updatedClientEntity);
        return clientMapper.modelToClientDetailsResponseDto(updatedModel);
    }
}