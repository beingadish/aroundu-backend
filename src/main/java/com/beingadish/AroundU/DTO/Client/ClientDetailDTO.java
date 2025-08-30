package com.beingadish.AroundU.DTO.Client;

import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.User.UserDetailDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClientDetailDTO extends UserDetailDTO {
    private List<AddressDTO> savedAddresses;
    private List<Long> postedJobIds;
}
