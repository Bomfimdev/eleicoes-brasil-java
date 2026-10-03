package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "cities")
public class City {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String provider;

  @Column(name = "state_code", nullable = false)
  private String stateCode;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @Column(name = "ibge_code")
  private String ibgeCode;

  @Column(nullable = false)
  private String name;

  @Column(name = "search_name", nullable = false)
  private String searchName;

  @Column(name = "is_capital", nullable = false)
  private boolean capital;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false, columnDefinition = "text[]")
  private String[] zones;
}
