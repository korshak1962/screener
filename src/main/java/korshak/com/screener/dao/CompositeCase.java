package korshak.com.screener.dao;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "composite_case")
@IdClass(CompositeCase.CompositeCaseId.class)
public class CompositeCase {

  @Id
  @Column(name = "case_id", nullable = false)
  private String caseId;

  @Id
  @Column(name = "composite_id", nullable = false)
  private String compositeId;


  public CompositeCase() {
  }

  public CompositeCase(String caseId, String compositeId) {
    this.caseId = caseId;
    this.compositeId = compositeId;
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public String getCompositeId() {
    return compositeId;
  }

  public void setCompositeId(String compositeId) {
    this.compositeId = compositeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompositeCase that = (CompositeCase) o;
    return Objects.equals(caseId, that.caseId) &&
        Objects.equals(compositeId, that.compositeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseId, compositeId);
  }

  @Override
  public String toString() {
    return "CompositeCase{" +
        "caseId='" + caseId + '\'' +
        ", compositeId='" + compositeId + '\'' +
        '}';
  }

  /**
   * ID class for the composite primary key
   * We still need this class for JPA to work with the composite primary key
   */
  public static class CompositeCaseId implements Serializable {
    private String caseId;
    private String compositeId;

    public CompositeCaseId() {
    }

    public CompositeCaseId(String caseId, String compositeId) {
      this.caseId = caseId;
      this.compositeId = compositeId;
    }

    public String getCaseId() {
      return caseId;
    }

    public void setCaseId(String caseId) {
      this.caseId = caseId;
    }

    public String getCompositeId() {
      return compositeId;
    }

    public void setCompositeId(String compositeId) {
      this.compositeId = compositeId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CompositeCaseId that = (CompositeCaseId) o;
      return Objects.equals(caseId, that.caseId) &&
          Objects.equals(compositeId, that.compositeId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(caseId, compositeId);
    }
  }
}