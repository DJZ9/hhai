package tech.aiflowy.ai.dto;

import java.math.BigInteger;
import java.util.List;

/**
 * 智能体挂载模块摘要（仅 id 和名称），用于多个智能体之间的对比。
 */
public class BotMountedModulesDTO {

    private BigInteger botId;
    private String botTitle;
    private List<Module> workflows;
    private List<Module> knowledges;
    private List<Module> plugins;
    private List<Module> mcps;
    private List<Module> skills;
    private List<Module> wikis;
    private List<Module> subagents;
    private List<Module> models;

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public String getBotTitle() {
        return botTitle;
    }

    public void setBotTitle(String botTitle) {
        this.botTitle = botTitle;
    }

    public List<Module> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Module> workflows) {
        this.workflows = workflows;
    }

    public List<Module> getKnowledges() {
        return knowledges;
    }

    public void setKnowledges(List<Module> knowledges) {
        this.knowledges = knowledges;
    }

    public List<Module> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<Module> plugins) {
        this.plugins = plugins;
    }

    public List<Module> getMcps() {
        return mcps;
    }

    public void setMcps(List<Module> mcps) {
        this.mcps = mcps;
    }

    public List<Module> getSkills() {
        return skills;
    }

    public void setSkills(List<Module> skills) {
        this.skills = skills;
    }

    public List<Module> getWikis() {
        return wikis;
    }

    public void setWikis(List<Module> wikis) {
        this.wikis = wikis;
    }

    public List<Module> getSubagents() {
        return subagents;
    }

    public void setSubagents(List<Module> subagents) {
        this.subagents = subagents;
    }

    public List<Module> getModels() {
        return models;
    }

    public void setModels(List<Module> models) {
        this.models = models;
    }

    /**
     * 挂载模块摘要：仅 id 和名称。
     */
    public static class Module {
        private BigInteger id;
        private String name;

        public Module() {
        }

        public Module(BigInteger id, String name) {
            this.id = id;
            this.name = name;
        }

        public BigInteger getId() {
            return id;
        }

        public void setId(BigInteger id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}