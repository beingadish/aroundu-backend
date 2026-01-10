package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterResponseDTO;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Exceptions.Client.ClientAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Client.ClientNotFoundException;
import com.beingadish.AroundU.Mappers.User.Client.ClientMapper;
import com.beingadish.AroundU.Models.ClientModel;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
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
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ClientRegisterResponseDTO registerClient(ClientRegisterRequestDTO requestDTO) {
        // Convert the RequestDTO to ClientModel
        ClientModel clientModel = clientMapper.registerRequestDtoToModel(requestDTO);

        // Validating if Client does not already exist
        Optional<Client> alreadyExistClient = clientRepository.findByEmail(clientModel.getEmail());

        if (alreadyExistClient.isPresent()) {
            throw new ClientAlreadyExistException("Client with the given email already exist.");
        }

        clientModel.setHashedPassword(passwordEncoder.encode(requestDTO.getPassword()));
        // Save the client entity in the database
        Client savedClient = clientRepository.save(clientMapper.modelToEntity(clientModel));

        return new ClientRegisterResponseDTO("Client is successfully created");
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailsResponseDTO getClientDetails(Long clientId) {
        Optional<Client> clientOptional = clientRepository.findById(clientId);
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
        Page<Client> pageData = clientRepository.findAll(pageable);
        return pageData.map(clientMapper::entityToModel).map(clientMapper::modelToClientDetailsResponseDto);
    }

//    @Override
//    public ClientResponseDTO getClientDetails(@NonNull ClientRequestDTO clientRequestDTO) {
//        // Extract email from the request DTO
//        String email = clientRequestDTO.getClientEmail();
//        String password = clientRequestDTO.getPassword();
//
//        // Use the repository method to fetch client details
//        ClientEntity clientEntity = clientRepository.findByEmail(email).orElseThrow(() -> new ClientNotFoundException("Client not found"));
//
//        // Convert the client entity to ClientResponseDTO and return
//        return DTOConversionUtil.clientEntityToClientResponseDto(clientEntity, "Client found");
//    }
//
//    @Override
//    public ClientResponseDTO updateClientDetails(@NonNull ClientRequestDTO clientRequestDTO) {
//        // Extract details from clientRequestDTO
//        Long clientId = clientRequestDTO.getClientId();
//
//        // Finding the Client Entity using Email & Password
//        ClientEntity foundClientEntity = clientRepository.findById(clientId).orElseThrow(() -> new ClientValidationException("Cannot update, client not found"));
//
//        // If found then set the incoming name
//        foundClientEntity.setClientName(clientRequestDTO.getClientName());
//        foundClientEntity.setClientEmail(clientRequestDTO.getClientEmail());
//        foundClientEntity.setPassword(clientRequestDTO.getPassword());
//
//        // Update it in the Database
//        ClientEntity updatedClientEntity = clientRepository.save(foundClientEntity);
//
//        // Convert the updated entity to a DTO and return
//        return DTOConversionUtil.clientEntityToClientResponseDto(updatedClientEntity, "Client Updated Successfully");
//    }
}