# Modelo de lenguaje utilizado por los agentes de OpenCode

Este proyecto está impulsado por el modelo **groq/openai/gpt‑oss‑120b** (identificado internamente como `openai/gpt-oss-120b`).

- **Arquitectura**: modelo de tipo **GPT** (Generative Pre‑trained Transformer) de gran escala.
- **Entrenamiento**: entrenado por **OpenAI** con una amplia variedad de datos de texto para generar respuestas coherentes y contextuales.
- **Proveedor**: el modelo está disponible a través de la plataforma **Groq**, que ofrece inferencia de baja latencia.
- **Uso en el proyecto**: todos los sub‑agentes (`@explore`, `@review`, `@audit`, `@docs-writer`, etc.) utilizan este mismo modelo para razonamiento, generación de código y documentación.

Esta información ayuda a comprender las capacidades y limitaciones del asistente que genera la documentación y el código del proyecto.
