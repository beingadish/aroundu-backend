package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Client.ClientRequestDTO;
import com.beingadish.AroundU.DTO.Client.ClientResponseDTO;
import org.springframework.stereotype.Service;

@Service
public interface ClientService {

    ClientResponseDTO registerClient(ClientRequestDTO clientRequestDTO);

    ClientResponseDTO getClientDetails(ClientRequestDTO clientRequestDTO);

    ClientResponseDTO updateClientDetails(ClientRequestDTO clientRequestDTO);

}
