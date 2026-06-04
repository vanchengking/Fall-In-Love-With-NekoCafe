<template>
  <div class="chips">
    <el-check-tag
      v-for="tag in options"
      :key="tag"
      :checked="modelValue.includes(tag)"
      @change="toggle(tag)"
    >
      {{ tag }}
    </el-check-tag>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  modelValue: string[]
  options: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  change: []
}>()

function toggle(tag: string) {
  const next = props.modelValue.includes(tag)
    ? props.modelValue.filter((t) => t !== tag)
    : [...props.modelValue, tag]
  emit('update:modelValue', next)
  emit('change')
}
</script>
