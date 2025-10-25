# Correção do Bug de Cache de Imagem

## Problema Identificado

O app estava apresentando um bug onde, ao selecionar uma nova imagem para editar, a imagem anterior continuava sendo exibida. Isso acontecia porque:

1. O usuário selecionava uma imagem A → o editor carregava a imagem A
2. O usuário voltava para a galeria → o ViewModel permanecia na memória com a imagem A
3. O usuário selecionava uma imagem B → o mesmo ViewModel era reutilizado, mantendo a imagem A em cache

## Causa Raiz

O problema estava na `MainActivity.kt` onde o `hiltViewModel()` estava sendo reutilizado entre diferentes sessões de edição. O Hilt Dependency Injection estava mantendo a mesma instância do `ImageEditorViewModel` no escopo da Activity, causando o cache indevido das imagens.

## Solução Implementada

### 1. Chaves Únicas para ViewModels

Adicionamos chaves únicas para cada instância do ViewModel baseadas na imagem sendo editada:

```kotlin
// Para imagens novas
val editorViewModel: ImageEditorViewModel = hiltViewModel(key = "editor_${screen.imageUri}_${screen.fileName}")

// Para imagens salvas
val editorViewModel: ImageEditorViewModel = hiltViewModel(key = "editor_saved_${screen.savedImage.id}")
```

### 2. Reset do Estado do Editor

Adicionamos chamadas para resetar o estado do editor:

- **Ao entrar no editor**: `editorViewModel.handleAction(ImageEditorAction.ResetEditor)`
- **Ao sair do editor**: Reset também é chamado no `onNavigateBack`

### 3. Carregamento Limpo da Imagem

O `LaunchedEffect` agora garante que:
1. O estado do editor seja limpo primeiro
2. A nova imagem seja carregada em um estado limpo

## Código Modificado

### MainActivity.kt

```kotlin
is Screen.Editor -> {
    val editorViewModel: ImageEditorViewModel = hiltViewModel(key = "editor_${screen.imageUri}_${screen.fileName}")
    
    // Reset editor state and load the image when entering editor screen
    androidx.compose.runtime.LaunchedEffect(screen.imageUri) {
        editorViewModel.handleAction(ImageEditorAction.ResetEditor)
        editorViewModel.loadImage(screen.imageUri, screen.fileName)
    }
    
    ImageEditorScreen(
        onNavigateBack = { 
            // Clear the editor state when navigating back
            editorViewModel.handleAction(ImageEditorAction.ResetEditor)
            currentScreen = Screen.Gallery 
        },
        modifier = Modifier.fillMaxSize(),
        viewModel = editorViewModel
    )
}
```

## Benefícios da Solução

1. **Isolamento de Sessões**: Cada sessão de edição tem seu próprio ViewModel isolado
2. **Estado Limpo**: O estado é sempre resetado ao entrar/sair do editor
3. **Prevenção de Cache**: Elimina completamente o problema de cache de imagens antigas
4. **Compatibilidade**: Mantém toda a funcionalidade existente do app

## Teste da Correção

Para testar se a correção funcionou:

1. Selecione uma imagem A para editar
2. Volte para a galeria
3. Selecione uma imagem B diferente
4. Verifique se a imagem B é exibida corretamente (não a imagem A)
5. Teste também com o botão "+" para adicionar novas imagens

A correção garante que cada nova seleção de imagem resulte em um estado limpo do editor, eliminando o bug de cache.