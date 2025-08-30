package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Entities.ClientEntity;
import com.beingadish.AroundU.Exceptions.Client.ClientAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Client.ClientNotFoundException;
import com.beingadish.AroundU.Exceptions.Client.ClientValidationException;
import com.beingadish.AroundU.Repository.ClientRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientServiceImpl implements ClientService {


    @Autowired
    private ClientRepository clientRepository;

    @Override
    public ClientResponseDTO registerClient(ClientRequestDTO clientRequestDTO) {
        // Convert the ClientRequestDTO to ClientEntity
        ClientEntity clientEntity = DTOConversionUtil.clientRequestDtoToClientEntity(clientRequestDTO);

        // Validating if Client does not already exist
        Optional<ClientEntity> alreadyExistClient = clientRepository
                .findByEmail(clientEntity.getClientEmail());

        if(alreadyExistClient.isPresent()){
            throw new ClientAlreadyExistException("Client with the given email already exist.");
        }

        // Save the client entity in the database
        ClientEntity savedClient = clientRepository.save(clientEntity);
        // Convert the saved entity back to ClientResponseDTO
        return DTOConversionUtil.clientEntityToClientResponseDto(savedClient, "Client Registered Successfully");
    }

    @Override
    public ClientResponseDTO getClientDetails(@NonNull ClientRequestDTO clientRequestDTO) {
        // Extract email from the request DTO
        String email = clientRequestDTO.getClientEmail();
        String password = clientRequestDTO.getPassword();

        // Use the repository method to fetch client details
        ClientEntity clientEntity = clientRepository
                .findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

        // Convert the client entity to ClientResponseDTO and return
        return DTOConversionUtil.clientEntityToClientResponseDto(clientEntity, "Client found");
    }

    @Override
    public ClientResponseDTO updateClientDetails(@NonNull ClientRequestDTO clientRequestDTO) {
        // Extract details from clientRequestDTO
        Long clientId = clientRequestDTO.getClientId();

        // Finding the Client Entity using Email & Password
        ClientEntity foundClientEntity = clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ClientValidationException("Cannot update, client not found"));

        // If found then set the incoming name
        foundClientEntity.setClientName(clientRequestDTO.getClientName());
        foundClientEntity.setClientEmail(clientRequestDTO.getClientEmail());
        foundClientEntity.setPassword(clientRequestDTO.getPassword());

        // Update it in the Database
        ClientEntity updatedClientEntity = clientRepository.save(foundClientEntity);

        // Convert the updated entity to a DTO and return
        return DTOConversionUtil.clientEntityToClientResponseDto(updatedClientEntity, "Client Updated Successfully");
    }
}
