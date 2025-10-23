# Funcionalidade de Aplicação Automática de Filtros

## Resumo das Mudanças

Implementei a funcionalidade solicitada onde:
1. **Aplicação automática**: Quando o usuário seleciona um filtro (ex: sepia), ele é aplicado automaticamente
2. **Replicação com delay**: Quando o usuário move a barra de intensidade, o filtro é reaplicado após 1 segundo de inatividade
3. **Botão dinâmico**: O botão muda de "Apply" para "Done" quando um filtro já está aplicado

## Arquivos Modificados

### 1. `ImageEditorUiState.kt`
- Adicionado `isFilterApplied: Boolean` para rastrear se um filtro está aplicado como preview
- Adicionado `previewImage: Bitmap?` para armazenar a imagem com filtro aplicado
- Adicionadas novas ações: `ApplyFilterPreview` e `CommitFilterPreview`

### 2. `ImageEditorViewModel.kt`
- Adicionado `filterPreviewJob: Job?` para controlar o debounce de 1 segundo
- Modificado `selectFilter()`: aplica automaticamente o preview quando um filtro é selecionado
- Modificado `setFilterIntensity()`: cancela job anterior e inicia novo com delay de 1 segundo
- Adicionado `applyFilterPreview()`: aplica filtro como preview sem salvar no histórico
- Adicionado `commitFilterPreview()`: confirma o filtro e salva no histórico
- Modificado `applyFilter()`: agora funciona como commit se já há preview aplicado

### 3. `ImageEditorScreen.kt`
- Modificado `ImageDisplayArea`: mostra `previewImage` quando disponível, senão mostra `editedImage`
- Modificado `FilterControlPanel`: aceita parâmetro `isFilterApplied`
- Modificado `FilterPanel`: botão muda de "Apply"/"Check" para "Done"/"Done icon" quando filtro está aplicado

## Como Funciona

### Fluxo de Aplicação Automática
1. Usuário seleciona filtro (ex: Sepia)
2. `selectFilter()` é chamado
3. Automaticamente chama `applyFilterPreview()`
4. Filtro é aplicado e armazenado em `previewImage`
5. `isFilterApplied` vira `true`
6. Interface mostra a imagem filtrada e botão "Done"

### Fluxo de Mudança de Intensidade
1. Usuário move slider de intensidade
2. `setFilterIntensity()` é chamado
3. Job anterior é cancelado
4. Novo job é iniciado com delay de 1 segundo
5. Após 1 segundo, `applyFilterPreview()` é chamado
6. Filtro é reaplicado com nova intensidade

### Fluxo de Confirmação
1. Usuário clica em "Done"
2. `applyFilter()` é chamado
3. Como `isFilterApplied` é `true`, chama `commitFilterPreview()`
4. Preview é confirmado, salvo no histórico
5. Estados são limpos e ferramenta é fechada

## Benefícios

- **UX melhorada**: Usuário vê resultado imediatamente
- **Performance otimizada**: Debounce evita processamento excessivo durante ajuste de intensidade
- **Feedback visual claro**: Botão "Done" indica que filtro já está aplicado
- **Compatibilidade**: Mantém funcionalidade existente de undo/redo e histórico

## Comportamento Esperado

1. **Seleção de filtro**: Sepia é aplicado instantaneamente
2. **Ajuste de intensidade**: Após parar de mover por 1 segundo, filtro é reaplicado
3. **Botão dinâmico**: Mostra "Done" em vez de "Apply"
4. **Confirmação**: Clique em "Done" confirma e fecha a ferramenta