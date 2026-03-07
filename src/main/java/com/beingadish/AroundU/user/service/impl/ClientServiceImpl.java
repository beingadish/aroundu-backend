package com.beingadish.AroundU.user.service.impl;

import com.beingadish.AroundU.common.dto.AddressDTO;
import com.beingadish.AroundU.common.mapper.AddressMapper;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.location.repository.AddressRepository;
import com.beingadish.AroundU.user.dto.client.ClientDetailsResponseDTO;
import com.beingadish.AroundU.user.dto.client.ClientRegisterRequestDTO;
import com.beingadish.AroundU.user.dto.client.ClientUpdateRequestDTO;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.exception.ClientAlreadyExistException;
import com.beingadish.AroundU.user.exception.ClientNotFoundException;
import com.beingadish.AroundU.user.exception.ClientValidationException;
import com.beingadish.AroundU.user.mapper.ClientMapper;
import com.beingadish.AroundU.user.model.ClientModel;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.ClientWriteRepository;
import com.beingadish.AroundU.user.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientMapper clientMapper;
    private final ClientReadRepository clientReadRepository;
    private final ClientWriteRepository clientWriteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressMapper addressMapper;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
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
        log.info("Registered client with email={} (id assigned by DB)", clientModel.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailsResponseDTO getClientDetails(Long clientId) {
        Client clientEntity = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with id %d does not exists".formatted(clientId)));

        log.debug("Fetched client details for id={}", clientId);
        return mapToResponse(clientEntity);
    }

    private ClientDetailsResponseDTO mapToResponse(Client entity) {
        ClientModel model = clientMapper.entityToModel(entity);
        ClientDetailsResponseDTO dto = clientMapper.modelToClientDetailsResponseDto(model);
        dto.setSavedAddresses(
                entity.getSavedAddresses() == null
                ? java.util.List.of()
                : addressMapper.toDtoList(entity.getSavedAddresses())
        );
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDetailsResponseDTO> getAllClients(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Client> pageData = clientReadRepository.findAll(pageable);
        return pageData.map(this::mapToResponse);
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

        if (updateRequest.getCurrency() != null) {
            foundClientEntity.setCurrency(updateRequest.getCurrency());
        }

        if (updateRequest.getCountry() != null && !updateRequest.getCountry().isBlank()) {
            foundClientEntity.setCountry(updateRequest.getCountry().trim().toUpperCase());
        }

        Client updatedClientEntity = clientWriteRepository.save(foundClientEntity);
        log.info("Updated client details for id={} email={}", clientId, updatedClientEntity.getEmail());
        return mapToResponse(updatedClientEntity);
    }

    @Override
    @Transactional
    public void deleteClient(Long clientId) {
        Client client = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with id %d does not exists".formatted(clientId)));

        clientWriteRepository.deleteById(clientId);
        log.info("Deleted client id={} email={}", clientId, client.getEmail());
    }

    @Override
    @Transactional
    public ClientDetailsResponseDTO addSavedAddress(Long clientId, AddressDTO addressDTO) {
        Client client = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with id %d does not exist".formatted(clientId)));

        Address address = addressMapper.toEntity(addressDTO);
        address.setClient(client);
        if (client.getSavedAddresses() == null) {
            client.setSavedAddresses(new java.util.ArrayList<>());
        }
        client.getSavedAddresses().add(address);
        Client saved = clientWriteRepository.save(client);
        log.info("Added saved address for client id={}", clientId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ClientDetailsResponseDTO deleteSavedAddress(Long clientId, Long addressId) {
        Client client = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with id %d does not exist".formatted(clientId)));

        if (client.getSavedAddresses() == null || client.getSavedAddresses().isEmpty()) {
            throw new ClientValidationException("No saved addresses found");
        }

        boolean removed = client.getSavedAddresses().removeIf(a -> a.getAddressId().equals(addressId));
        if (!removed) {
            throw new ClientValidationException("Address with id %d not found in saved addresses".formatted(addressId));
        }

        Client saved = clientWriteRepository.save(client);
        log.info("Deleted saved address id={} for client id={}", addressId, clientId);
        return mapToResponse(saved);
    }
}
