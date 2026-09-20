import docNode from './documentNode';
import downloadNode from './downloadNode';
import { ImageGenerationNode } from './imageGenerationNode';
import { VideoGenerationNode } from './videoGenerationNode';
import makeFileNode from './makeFileNode';
import { McpNode } from './mcpNode';
import nodeNames from './nodeNames';
import { OcrContentExtractionNode } from './ocrContentExtractionNode';
import { PluginNode } from './pluginNode';
import { SaveToDatacenterNode } from './saveToDatacenter';
import { SearchDatacenterNode } from './searchDatacenter';
import sqlNode from './sqlNode';
import { WorkflowNode } from './workflowNode';

export interface CustomNodeOptions {
  handleChosen?: (
    nodeType: string,
    updateNodeData: any,
    value: string,
    label?: string,
  ) => void;
}
export const getCustomNode = async (options: CustomNodeOptions) => {
  const pluginNode = PluginNode({ onChosen: options.handleChosen });
  const mcpNode = McpNode({ onChosen: options.handleChosen });
  const workflowNode = WorkflowNode({ onChosen: options.handleChosen });
  const searchDatacenterNode = await SearchDatacenterNode();
  const saveToDatacenterNode = await SaveToDatacenterNode();
  const ocrContentExtractionNode = await OcrContentExtractionNode();
  const imageGenerationNode = await ImageGenerationNode();
  const videoGenerationNode = await VideoGenerationNode();
  return {
    ...docNode,
    ...makeFileNode,
    ...downloadNode,
    ...sqlNode,
    [nodeNames.pluginNode]: pluginNode,
    [nodeNames.mcpNode]: mcpNode,
    [nodeNames.workflowNode]: workflowNode,
    [nodeNames.searchDatacenterNode]: searchDatacenterNode,
    [nodeNames.saveToDatacenterNode]: saveToDatacenterNode,
    [nodeNames.ocrContentExtractionNode]: ocrContentExtractionNode,
    [nodeNames.imageGenerationNode]: imageGenerationNode,
    [nodeNames.videoGenerationNode]: videoGenerationNode,
  };
};
