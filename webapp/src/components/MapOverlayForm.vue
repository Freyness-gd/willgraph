<template>
	<div class="map-overlay-form">
		<q-input v-model="field1" class="overlay-input" dense filled label="Field 1" />
		<q-input v-model="field2" class="overlay-input" dense filled label="Field 2" />
		<q-input v-model="field3" class="overlay-input" dense filled label="Field 3" />
	</div>
</template>

<script lang="ts" setup>
import { ref } from "vue";

const field1 = ref("");
const field2 = ref("");
const field3 = ref("");
</script>

<style scoped>
.map-overlay-form {
	position: fixed;
	top: 80px;
	right: 20px;
	z-index: 1000;
	display: flex;
	flex-direction: column;
	gap: 8px;
	padding: 16px;
	background-color: rgba(255, 255, 255, 0.3);
	border-radius: 8px;
	backdrop-filter: blur(4px);
}

.overlay-input {
	background-color: white;
	border-radius: 4px;
	min-width: 200px;
}

.overlay-input :deep(.q-field__control) {
	background-color: white !important;
}
</style>
