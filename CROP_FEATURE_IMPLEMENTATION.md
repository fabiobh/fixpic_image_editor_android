# Implementação da Funcionalidade de Crop

## Resumo
Foi implementada uma funcionalidade completa de crop (corte) de imagem no aplicativo FixPic, permitindo que os usuários selecionem e cortem áreas específicas de suas imagens de forma interativa.

## Componentes Implementados

### 1. **EditingTool.kt**
- Adicionado o enum `Crop` à lista de ferramentas de edição disponíveis
- Agora o app suporta: None, Crop, Resize e Filter

### 2. **ImageEditorUiState.kt**
- Adicionadas novas ações:
  - `SetCropBounds(bounds: Rect)` - Define os limites da área de corte
  - `ApplyCrop` - Aplica o corte à imagem

### 3. **CropOverlay.kt** (Novo arquivo)
Componente visual interativo que permite ao usuário:
- Visualizar a área de corte com overlay escurecido nas áreas não selecionadas
- Arrastar os cantos para redimensionar a área de corte
- Mover toda a área de corte arrastando o centro
- Ver linhas de grade (regra dos terços) para melhor composição
- Handles visuais nos cantos para facilitar o redimensionamento

**Características técnicas:**
- Usa `Canvas` do Compose para desenho customizado
- Implementa `detectDragGestures` para interação touch
- Calcula automaticamente o fator de escala para ajustar a imagem na tela
- Limita a área de corte aos limites da imagem
- Tamanho mínimo de corte de 50x50 pixels

### 4. **ImageEditorViewModel.kt**
Adicionadas funções:
- `setCropBounds(bounds: Rect)` - Atualiza o estado com os novos limites de corte
- `applyCrop()` - Executa o corte da imagem usando o `CropImageUseCase`
- Integração com o sistema de histórico (undo/redo)
- Tratamento de erros durante o processo de corte

### 5. **ImageEditorScreen.kt**
Atualizações na UI:
- Adicionado botão "Crop" na toolbar principal
- Criado `CropControlPanel` que mostra:
  - Tamanho atual da imagem
  - Instruções de uso
  - Dimensões da área de corte selecionada
  - Botões "Cancel" e "Apply"
- Integrado `CropOverlay` na área de exibição da imagem
- O overlay só aparece quando a ferramenta Crop está selecionada

### 6. **Strings Resources**
As strings já existiam nos arquivos de recursos:
- Inglês: `app/src/main/res/values/strings.xml`
- Português: `app/src/main/res/values-pt-rBR/strings.xml`

Strings relacionadas ao crop:
- `tool_crop` - "Crop" / "Cortar"
- `crop_tool` - "Crop tool" / "Ferramenta de corte"
- `crop_area` - "Crop area: %1$d × %2$d" / "Área de corte: %1$d × %2$d"
- `drag_to_adjust_crop` - "Drag to adjust crop area" / "Arraste para ajustar a área de corte"
- `crop_handle` - "Crop handle" / "Alça de corte"

## Como Usar

1. **Abrir uma imagem** no editor
2. **Selecionar a ferramenta Crop** tocando no botão "Crop" na toolbar
3. **Ajustar a área de corte**:
   - Arrastar os cantos para redimensionar
   - Arrastar o centro para mover a área
   - Usar as linhas de grade como guia de composição
4. **Aplicar o corte** tocando no botão "Apply"
5. **Cancelar** tocando no botão "Cancel" se desejar descartar as alterações

## Funcionalidades Técnicas

### Gestão de Estado
- O estado do crop é gerenciado no `ImageEditorUiState`
- Suporta undo/redo através do sistema de histórico
- Preserva as operações aplicadas para reconstrução da imagem

### Performance
- Processamento assíncrono usando coroutines
- Indicador de progresso durante o processamento
- Validação de limites para evitar erros

### Integração
- Funciona em conjunto com outras ferramentas (Resize, Filter)
- Mantém a ordem das operações aplicadas
- Suporta múltiplas operações de crop no histórico

## Arquitetura

A implementação segue a arquitetura MVVM do app:
- **UI Layer**: Composables em `ImageEditorScreen.kt` e `CropOverlay.kt`
- **ViewModel**: Lógica de negócio em `ImageEditorViewModel.kt`
- **Domain Layer**: Use case `CropImageUseCase` (já existente)
- **Data Layer**: Implementação em `ImageProcessingRepositoryImpl.kt` (já existente)

## Melhorias Futuras Possíveis

1. **Proporções predefinidas**: Adicionar botões para proporções comuns (1:1, 4:3, 16:9, etc.)
2. **Rotação**: Permitir rotação da área de corte
3. **Zoom**: Adicionar capacidade de zoom para cortes mais precisos
4. **Gestos**: Suporte a pinch-to-zoom na área de corte
5. **Presets**: Salvar áreas de corte favoritas
6. **Guias adicionais**: Opções de diferentes tipos de grade (diagonal, espiral dourada, etc.)

## Testes

Para testar a funcionalidade:
1. Compile e execute o app
2. Selecione ou tire uma foto
3. Toque no botão "Crop" na toolbar
4. Experimente arrastar os cantos e o centro da área de corte
5. Observe as dimensões atualizando em tempo real
6. Aplique o corte e verifique o resultado
7. Use undo para reverter se necessário
