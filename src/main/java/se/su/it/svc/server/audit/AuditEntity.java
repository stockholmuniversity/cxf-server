package se.su.it.svc.server.audit;

import java.util.List;

public class AuditEntity {

  String created;
  String operation;
  String text_args;
  String raw_args;
  String text_return;
  String raw_return;
  String state;
  List<String> methodDetails;

  private AuditEntity() {}

  public static AuditEntity getInstance(String created,
                                        String operation,
                                        String text_args,
                                        String raw_args,
                                        String text_return,
                                        String raw_return,
                                        String state,
                                        List<String> methodDetails) {

    AuditEntity auditEntity = new AuditEntity();
    auditEntity.created       = created;
    auditEntity.operation     = operation;
    auditEntity.text_args     = text_args;
    auditEntity.raw_args      = raw_args;
    auditEntity.text_return   = text_return;
    auditEntity.raw_return    = raw_return;
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
    sb.append("text_args:").append(text_args).append(", ");
    sb.append("text_return:").append(text_return).append(", ");
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
