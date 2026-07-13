package com.print3dmanager.erp.user.mapper;

import com.print3dmanager.erp.user.dto.UserCreateRequest;
import com.print3dmanager.erp.user.dto.UserResponse;
import com.print3dmanager.erp.user.dto.UserUpdateRequest;
import com.print3dmanager.erp.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO (componentModel spring via flag global
 * do compilador). A senha nunca passa pelo mapper: é codificada com
 * BCrypt no service.
 */
@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "senha", ignore = true)
    User toEntity(UserCreateRequest request);

    void atualizar(@MappingTarget User user, UserUpdateRequest request);
}
