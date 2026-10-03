package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {

  List<City> findByProviderAndStateCodeOrderByNameAsc(String provider, String stateCode);

  List<City> findByProviderAndCapitalTrueOrderByStateCodeAsc(String provider);

  Optional<City> findByProviderAndStateCodeAndProviderId(
      String provider, String stateCode, String providerId);
}
