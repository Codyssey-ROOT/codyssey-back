
package site.defence.domain.image.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.global.entity.BaseTimeEntity;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(name = "ai_model")
@NoArgsConstructor(access = PROTECTED)
public class AiModel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "ai_model_id")
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Builder(access = PRIVATE)
    private AiModel(String modelName, RiskLevel riskLevel) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("AI 모델명은 필수입니다.");
        }

        this.modelName = modelName;
        this.riskLevel = riskLevel == null ? RiskLevel.SAFE : riskLevel;
    }

    public static AiModel of(String modelName, RiskLevel riskLevel) {
        return AiModel.builder()
                .modelName(modelName)
                .riskLevel(riskLevel)
                .build();
    }

    public void updateRiskLevel(RiskLevel riskLevel) {
        if (riskLevel == null) {
            throw new IllegalArgumentException("위험 등급은 필수입니다.");
        }

        this.riskLevel = riskLevel;
    }
}
