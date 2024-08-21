package com.github.mirum8.jnscli.shell;

import jakarta.annotation.Nullable;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.component.SingleItemSelector;
import org.springframework.shell.component.SingleItemSelector.SingleItemSelectorContext;
import org.springframework.shell.component.StringInput;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShellPrompter extends AbstractShellComponent {
    private final LineReader lineReader;

    @Autowired
    public ShellPrompter(@Lazy LineReader lineReader) {
        this.lineReader = lineReader;
    }

    public String promptPassword(String prompt) {
        return promptString(prompt, null, true);
    }

    public String promptString(String prompt, String defaultValue) {
        return promptString(prompt, defaultValue, false);
    }

    public String promptString(String prompt, String defaultValue, boolean mask) {
        StringInput component = new StringInput(getTerminal(), prompt, defaultValue);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        if (mask) {
            component.setMaskCharacter('*');
        }
        StringInput.StringInputContext context = component.run(StringInput.StringInputContext.empty());
        return context.getResultValue();
    }

    public String promptSelectFromList(@Nullable String headingMessage, List<String> values) {
        List<SelectorItem<String>> items = values.stream()
            .map(v -> SelectorItem.of(v, v))
            .toList();
        SingleItemSelector<String, SelectorItem<String>> singleItemSelector = new SingleItemSelector<>(
            getTerminal(),
            items,
            headingMessage,
            null
        );
        singleItemSelector.setResourceLoader(getResourceLoader());
        singleItemSelector.setTemplateExecutor(getTemplateExecutor());
        SingleItemSelectorContext<String, SelectorItem<String>> context = singleItemSelector.run(SingleItemSelectorContext.empty());
        return context.getResultItem().map(SelectorItem::getItem).orElse("");
    }


    public boolean promptForYesNo(String prompt, boolean defaultYes) {
        String answer = readAnswerForYesNo(prompt, defaultYes);
        return answer.isEmpty() ? defaultYes : answer.equalsIgnoreCase("y");
    }

    private String readAnswerForYesNo(String prompt, boolean defaultYes) {
        String answer;
        do {
            answer = lineReader.readLine(String.format("%s [%s/%s]: ", prompt, defaultYes ? "Y" : "y", defaultYes ? "n" : "N"));
        } while (!answer.equalsIgnoreCase("y") && !answer.equalsIgnoreCase("n") && !answer.isEmpty());
        return answer;
    }
}
