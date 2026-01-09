package com.beingadish.AroundU.DTO.Client.Details;

import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.User.UserDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailsRequestDTO extends UserDetailDTO {
    private List<AddressDTO> savedAddresses;
    private List<Long> postedJobIds;
}
