package ch.so.agi.dbeaver.ai.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public final class AskWithSelectionHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (page == null) {
            return null;
        }

        try {
            AiChatViewPart view = (AiChatViewPart) page.showView(AiChatViewPart.VIEW_ID);
            String selectedText = readSelection(event);
            if (!selectedText.isBlank()) {
                view.prefillPrompt("Bitte erkläre oder verbessere folgenden SQL/Text:\n" + selectedText);
            }
        } catch (PartInitException e) {
            throw new ExecutionException("Unable to open AI chat view", e);
        }

        return null;
    }

    private String readSelection(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof ITextSelection textSelection) {
            String text = textSelection.getText();
            return text == null ? "" : text.trim();
        }
        return "";
    }
}
