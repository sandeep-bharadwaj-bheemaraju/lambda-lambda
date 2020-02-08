package com.sandeep.learning.lambdalambda.handler;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LambdaResponse {

    private String requestId;

    private String jobRunId;

    private String eventTime;

    private String message;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getRequestId() != null)
            sb.append("\"requestId\" : ").append("\"").append(getRequestId()).append("\"").append(",");
        if (getJobRunId() != null)
            sb.append("\"jobRunId\" : ").append("\"").append(getJobRunId()).append("\"").append(",");
        if (getEventTime() != null)
            sb.append("\"eventTime\" : ").append("\"").append(getEventTime()).append("\"").append(",");
        if(getMessage() != null)
            sb.append("\"message\" : ").append("\"").append(getMessage()).append("\"");
        sb.append("}");
        return sb.toString();
    }
}
