package com.beingadish.AroundU.user.dto.client;

import com.beingadish.AroundU.common.dto.AddressDTO;
import com.beingadish.AroundU.user.dto.UserDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailsResponseDTO extends UserDetailDTO {

    private List<AddressDTO> savedAddresses;
}
