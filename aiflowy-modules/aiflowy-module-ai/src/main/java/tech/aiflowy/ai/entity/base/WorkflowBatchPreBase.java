package tech.aiflowy.ai.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


public class WorkflowBatchPreBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键")
    private BigInteger id;

    /**
     * 批次ID
     */
    @Column(comment = "批次ID")
    private BigInteger batchId;

    /**
     * 重试次数
     */
    @Column(comment = "重试次数")
    private Integer retryCount;

    /**
     * 执行参数
     */
    @Column(comment = "执行参数")
    private String input;

    /**
     * 执行参数显示
     */
    @Column(comment = "执行参数显示")
    private String inputStr;

    /**
     * 执行结果
     */
    @Column(comment = "执行结果")
    private String output;

    /**
     * 数据状态[1-执行中][2-执行完成][3-执行失败]
     */
    @Column(comment = "数据状态[1-执行中][2-执行完成][3-执行失败]")
    private Integer status;

    /**
     * 标题
     */
    @Column(comment = "标题")
    private String title;

    /**
     * 描述
     */
    @Column(comment = "描述")
    private String description;

    /**
     * 开始时间
     */
    @Column(comment = "开始时间")
    private Date startTime;

    /**
     * 结束时间
     */
    @Column(comment = "结束时间")
    private Date endTime;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getBatchId() {
        return batchId;
    }

    public void setBatchId(BigInteger batchId) {
        this.batchId = batchId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getInputStr() {
        return inputStr;
    }

    public void setInputStr(String inputStr) {
        this.inputStr = inputStr;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
