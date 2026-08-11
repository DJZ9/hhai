package tech.aiflowy.ai.tinyflow.event;

import java.math.BigInteger;
import java.util.Date;

public class PreEventEntity {

    private BigInteger preId;
    private Integer status;
    private String output;
    private Date startTime;
    private Date endTime;

    public BigInteger getPreId() {
        return preId;
    }

    public void setPreId(BigInteger preId) {
        this.preId = preId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
