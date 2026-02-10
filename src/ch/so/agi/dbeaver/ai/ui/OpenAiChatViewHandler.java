package ch.so.agi.dbeaver.ai.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public final class OpenAiChatViewHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (page == null) {
            return null;
        }

        try {
            page.showView(AiChatViewPart.VIEW_ID);
        } catch (PartInitException e) {
            throw new ExecutionException("Unable to open AI chat view", e);
        }

        return null;
    }
}
