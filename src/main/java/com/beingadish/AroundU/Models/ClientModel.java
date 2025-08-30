package com.beingadish.AroundU.Models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClientModel extends UserModel {
    private List<Long> postedJobIds;
    private List<Long> savedAddressIds;
}
