package se.su.it.svc.server.audit;

import java.util.List;

public class AuditEntity {

  private String created;
  private String operation;
  private String textArgs;
  private String textReturn;
  private String state;
  private List<String> methodDetails;

  private AuditEntity() {}

  public static AuditEntity getInstance(String created,
                                        String operation,
                                        String textArgs,
                                        String textReturn,
                                        String state,
                                        List<String> methodDetails) {

    AuditEntity auditEntity = new AuditEntity();
    auditEntity.created       = created;
    auditEntity.operation     = operation;
    auditEntity.textArgs      = textArgs;
    auditEntity.textReturn    = textReturn;
    auditEntity.state         = state;
    auditEntity.methodDetails = methodDetails;
    return auditEntity;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getClass().getName()).append("(");
    sb.append("created:").append(created).append(", ");
    sb.append("operation:").append(operation).append(", ");
    sb.append("text_args:").append(textArgs).append(", ");
    sb.append("text_return:").append(textReturn).append(", ");
    sb.append("state:").append(state).append(", ");

    sb.append("methodDetails:[");
    for (String detail : methodDetails) {
      sb.append(detail).append(", ");
    }
    sb.replace(sb.lastIndexOf(","), sb.length(), "").append("]");
    sb.append(")");

    return sb.toString();
  }
}
