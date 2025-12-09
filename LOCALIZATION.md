# Localização do Image Editor

Este documento descreve a implementação de localização do aplicativo Image Editor.

## Idiomas Suportados

- **Inglês (en)** - Idioma padrão
- **Português do Brasil (pt-BR)** - Localização completa

## Estrutura de Arquivos

```
app/src/main/res/
├── values/
│   └── strings.xml                 # Strings em inglês (padrão)
├── values-pt-rBR/
│   └── strings.xml                 # Strings em português do Brasil
└── xml/
    └── locales_config.xml          # Configuração de localização
```

## Configuração

### AndroidManifest.xml
- `android:supportsRtl="true"` - Suporte para idiomas RTL
- `android:localeConfig="@xml/locales_config"` - Configuração de localização

### Localização Automática
O Android automaticamente seleciona o idioma baseado nas configurações do dispositivo:
- Se o dispositivo estiver em português do Brasil → usa `values-pt-rBR/`
- Caso contrário → usa `values/` (inglês)

## Strings Localizadas

### Categorias de Strings
1. **Interface Principal**
   - Títulos de telas
   - Botões e ações
   - Mensagens de status

2. **Ferramentas de Edição**
   - Nomes de ferramentas (Crop, Resize, Filter)
   - Descrições e instruções
   - Validações e erros

3. **Filtros**
   - Nomes dos filtros (Brightness, Contrast, etc.)
   - Controles de intensidade

4. **Seleção de Imagens**
   - Mensagens do image picker
   - Validações de formato e tamanho
   - Erros de permissão

5. **Mensagens de Erro**
   - Erros de processamento
   - Erros de memória
   - Erros de arquivo

## Como Adicionar Novos Idiomas

1. Criar nova pasta: `app/src/main/res/values-[código-idioma]/`
2. Copiar `strings.xml` e traduzir todas as strings
3. Adicionar o idioma em `locales_config.xml`
4. Testar com dispositivo configurado no novo idioma

### Exemplo para Espanhol:
```
mkdir app/src/main/res/values-es/
# Copiar e traduzir strings.xml
# Adicionar <locale android:name="es"/> em locales_config.xml
```

## Boas Práticas

1. **Nunca usar strings hardcoded** - Sempre usar recursos de string
2. **Usar placeholders** - Para valores dinâmicos: `%1$s`, `%1$d`
3. **Considerar comprimento do texto** - Textos traduzidos podem ser mais longos
4. **Testar em diferentes idiomas** - Verificar layout e usabilidade
5. **Manter consistência** - Usar terminologia consistente

## Testando Localização

### No Emulador/Dispositivo:
1. Ir em Configurações → Sistema → Idiomas
2. Alterar para português do Brasil
3. Abrir o app e verificar se as strings estão traduzidas

### Forçar idioma no código (para testes):
```kotlin
val locale = Locale("pt", "BR")
Locale.setDefault(locale)
val config = Configuration()
config.setLocale(locale)
context.resources.updateConfiguration(config, context.resources.displayMetrics)
```

## Strings Pendentes de Localização

✅ **Concluído**: Todas as strings visíveis ao usuário estão localizadas, incluindo:
- Nomes das ferramentas: "Crop" → "Cortar", "Resize" → "Redimensionar", "Filter" → "Filtros"
- Strings hardcoded no código foram identificadas e movidas para recursos de string

## 🌐 Seletor de Idioma

✅ **Implementado**: Seletor de idioma na tela principal (Gallery)
- **Localização**: Ícone no canto superior direito da tela da galeria
- **Idiomas disponíveis**: 
  1. **English** (aparece primeiro)
  2. **Português (Brasil)**
- **Funcionalidade**: Mudança instantânea de idioma com reinicialização da activity
- **Indicador visual**: Ícone de check ao lado do idioma atual selecionado

## Manutenção

Ao adicionar novas features:
1. Adicionar strings em `values/strings.xml` (inglês)
2. Traduzir e adicionar em `values-pt-rBR/strings.xml`
3. Verificar se não há strings hardcoded no código
4. Testar em ambos os idiomas