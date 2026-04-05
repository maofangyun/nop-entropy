package com.seago.code.excel;

import io.nop.core.lang.json.JsonTool;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.VirtualFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典生成器：根据字典名生成对应的 dict.yaml 文件
 */
public class PoDictGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(PoDictGenerator.class);

    private final IPoDictService dictService;

    public PoDictGenerator(IPoDictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 遍历字典名集合，生成对应的 dict.yaml 资源
     */
    public void generateDictFiles(Set<String> dictNames) {
        if (dictNames == null || dictNames.isEmpty() || dictService == null) return;

        for (String dictName : dictNames) {
            LOG.info("PoDictGenerator: Generating dict file for {}", dictName);
            List<PoDictOption> options = dictService.getDictOptions(dictName);
            if (options == null || options.isEmpty()) {
                LOG.warn("PoDictGenerator: No data found for dict {}", dictName);
                continue;
            }

            Map<String, Object> dictMap = buildDictMap(dictName, options);
            saveToVfs(dictName, dictMap);
        }
    }

    private Map<String, Object> buildDictMap(String dictName, List<PoDictOption> options) {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("label", dictName);
        dict.put("locale", "zh-CN");

        List<Map<String, Object>> optionList = new ArrayList<>();
        for (PoDictOption option : options) {
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("label", option.getLabel());
            opt.put("value", option.getValue());
            optionList.add(opt);
        }
        dict.put("options", optionList);
        return dict;
    }

    private void saveToVfs(String dictName, Map<String, Object> dictMap) {
        // Nop 字典文件的 YAML 格式路径为 /dict/{dictName}.dict.yaml
        String vfsPath = "/dict/" + dictName + ".dict.yaml";
        try {
            LOG.debug("PoDictGenerator: Saving dict {} to VFS path {}", dictName, vfsPath);
            IResource resource = VirtualFileSystem.instance().getResource(vfsPath);
            
            String yaml = JsonTool.serializeToYaml(dictMap);

            if (resource.isReadOnly()) {
                LOG.warn("PoDictGenerator: VFS path {} is read-only, dict content: \n{}", vfsPath, yaml);
            } else {
                ResourceHelper.writeText(resource, yaml, "UTF-8");
                LOG.info("PoDictGenerator: Successfully saved dict {} to {}", dictName, vfsPath);
            }
        } catch (Exception e) {
            LOG.error("PoDictGenerator: Failed to save dict {} to VFS", dictName, e);
        }
    }
}
