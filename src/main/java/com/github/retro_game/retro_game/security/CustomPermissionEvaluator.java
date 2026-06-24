package com.github.retro_game.retro_game.security;

import com.github.retro_game.retro_game.cache.BodyInfoCache;
import com.github.retro_game.retro_game.repository.BodyRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
  private final BodyInfoCache bodyInfoCache;
  private final BodyRepository bodyRepository;

  public CustomPermissionEvaluator(BodyInfoCache bodyInfoCache, BodyRepository bodyRepository) {
    this.bodyInfoCache = bodyInfoCache;
    this.bodyRepository = bodyRepository;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    var user = (CustomUser) authentication.getPrincipal();
    var bodyId = (long) targetDomainObject;

    var perm = (String) permission;
    if (!"ACCESS".equals(perm)) {
      throw new IllegalArgumentException("Permission should be always 'ACCESS'");
    }

    var userId = user.getUserId();
    var infoOpt = bodyInfoCache.find(bodyId);
    if (infoOpt.isPresent() && infoOpt.get().getUserId() == userId) {
      return true;
    }

    return bodyRepository.existsByIdAndUser_Id(bodyId, userId);
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                               Object permission) {
    return false;
  }
}
