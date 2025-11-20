class FileUploader {
    constructor() {
        this.initializeElements();
        this.bindEvents();
        this.loadStats();
    }

    initializeElements() {
        this.elements = {
            fileInput: document.getElementById('fileInput'),
            uploadBtn: document.getElementById('uploadBtn'),
            status: document.getElementById('statusText'),
            progressBar: document.getElementById('progressBar'),
            progressFill: document.getElementById('progressFill'),
            result: document.getElementById('result'),
            downloadLink: document.getElementById('downloadLink'),
            fileCount: document.getElementById('fileCount'),
            totalSize: document.getElementById('totalSize')
        };
    }

    bindEvents() {
        this.elements.uploadBtn.addEventListener('click', () => this.uploadFile());
    }

    uploadFile() {
        const file = this.elements.fileInput.files[0];
        if (!file) {
            alert('Select a file');
            return;
        }

        this.setUploading(true);
        this.showProgress();

        const xhr = new XMLHttpRequest();

        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                this.updateProgress(percent);
            }
        });

        xhr.addEventListener('load', () => this.handleUploadResponse(xhr));
        xhr.addEventListener('error', () => this.handleUploadError('Network error'));

        xhr.open('POST', '/upload');
        xhr.setRequestHeader('X-File-Name', encodeURIComponent(file.name));
        xhr.send(file);
    }

    handleUploadResponse(xhr) {
        if (xhr.status === 200) {
            const data = JSON.parse(xhr.responseText);
            this.showDownloadLink(data.downloadUrl);
            this.elements.fileInput.value = '';
            this.loadStats();
        } else {
            this.handleUploadError('Server error');
        }
    }

    handleUploadError(message) {
        this.elements.status.textContent = 'Error uploading';
        this.setUploading(false);
    }

    setUploading(uploading) {
        this.elements.uploadBtn.disabled = uploading;
        this.elements.uploadBtn.textContent = uploading ? 'Uploading...' : 'Upload';
    }

    showProgress() {
        this.elements.progressBar.style.display = 'block';
        this.elements.progressFill.style.width = '0%';
    }

    updateProgress(percent) {
        this.elements.progressFill.style.width = percent + '%';
        this.elements.status.textContent = 'Uploading: ' + percent + '%';
    }

    showDownloadLink(downloadUrl) {
        const fullUrl = window.location.origin + downloadUrl;
        this.elements.downloadLink.href = fullUrl;
        this.elements.downloadLink.textContent = fullUrl;
        this.elements.result.style.display = 'block';
        this.elements.status.textContent = 'Download is complete!';
        this.setUploading(false);

        setTimeout(() => {
            this.elements.progressBar.style.display = 'none';
        }, 1000);
    }

    async loadStats() {
        try {
            const response = await fetch('/stats');
            const data = await response.json();
            this.elements.fileCount.textContent = data.fileCount;
            this.elements.totalSize.textContent = this.formatFileSize(data.totalSize);
        } catch (error) {
            console.log('Unable to load Stats');
        }
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new FileUploader();
});