package com.github.retro_game.retro_game.security;

import com.github.retro_game.retro_game.cache.BodyInfoCache;
import com.github.retro_game.retro_game.dto.BodyInfoDto;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.entity.UserRole;
import com.github.retro_game.retro_game.repository.BodyRepository;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomPermissionEvaluatorTest {
  @Test
  public void hasPermissionFallsBackToRepositoryWhenCacheMissesOwnedBody() {
    var bodyRepository = bodyRepository(true);
    var bodyInfoCache = bodyInfoCache(bodyRepository, Optional.empty());
    var evaluator = new CustomPermissionEvaluator(bodyInfoCache, bodyRepository);
    var authentication = authentication(7);

    assertThat(evaluator.hasPermission(authentication, 3L, "ACCESS")).isTrue();
  }

  @Test
  public void hasPermissionUsesCacheWhenItGrantsAccess() {
    var bodyRepository = bodyRepository(false);
    var bodyInfoCache = bodyInfoCache(bodyRepository, Optional.of(new BodyInfoDto(3, 7, "Colony", null)));
    var evaluator = new CustomPermissionEvaluator(bodyInfoCache, bodyRepository);
    var authentication = authentication(7);

    assertThat(evaluator.hasPermission(authentication, 3L, "ACCESS")).isTrue();
  }

  @Test
  public void hasPermissionDeniesWhenCacheAndRepositoryDoNotGrantAccess() {
    var bodyRepository = bodyRepository(false);
    var bodyInfoCache = bodyInfoCache(bodyRepository, Optional.empty());
    var evaluator = new CustomPermissionEvaluator(bodyInfoCache, bodyRepository);
    var authentication = authentication(7);

    assertThat(evaluator.hasPermission(authentication, 3L, "ACCESS")).isFalse();
  }

  @Test
  public void hasPermissionChecksRepositoryWhenCacheHasDifferentOwner() {
    var bodyRepository = bodyRepository(false);
    var bodyInfoCache = bodyInfoCache(bodyRepository, Optional.of(new BodyInfoDto(3, 8, "Colony", null)));
    var evaluator = new CustomPermissionEvaluator(bodyInfoCache, bodyRepository);
    var authentication = authentication(7);

    assertThat(evaluator.hasPermission(authentication, 3L, "ACCESS")).isFalse();
  }

  private static UsernamePasswordAuthenticationToken authentication(long userId) {
    var user = new User();
    ReflectionTestUtils.setField(user, "id", userId);
    user.setName("user" + userId);
    user.setEmail("user" + userId + "@test");
    user.setPassword("password");
    user.setRoles(UserRole.USER);
    var principal = new CustomUser(user);
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private static BodyInfoCache bodyInfoCache(BodyRepository bodyRepository, Optional<BodyInfoDto> info) {
    return new BodyInfoCache(bodyRepository) {
      @Override
      public Optional<BodyInfoDto> find(long bodyId) {
        return info;
      }
    };
  }

  private static BodyRepository bodyRepository(boolean existsByIdAndUserId) {
    return (BodyRepository) Proxy.newProxyInstance(
        BodyRepository.class.getClassLoader(),
        new Class<?>[]{BodyRepository.class},
        (proxy, method, args) -> {
          if (method.getName().equals("existsByIdAndUser_Id")) {
            return existsByIdAndUserId;
          }
          if (method.getReturnType().equals(boolean.class)) {
            return false;
          }
          if (method.getReturnType().equals(long.class)) {
            return 0L;
          }
          if (method.getReturnType().equals(int.class)) {
            return 0;
          }
          return null;
        });
  }
}
