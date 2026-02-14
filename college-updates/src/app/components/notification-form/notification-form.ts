import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationsService } from '../../services/notifications.service';

@Component({
  selector: 'app-notification-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './notification-form.html',
  styleUrl: './notification-form.css'
})
export class NotificationForm {
  @Output() notificationAdded = new EventEmitter<void>();

  newNotification = {
    message: ''
  };

  isSubmitting = false;
  showForm = false;
  error: string | null = null;

  constructor(private readonly notificationsService: NotificationsService) {}

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.resetForm();
    }
  }

  onSubmit(): void {
    if (!this.newNotification.message.trim()) {
      this.error = 'Message is required';
      return;
    }

    this.isSubmitting = true;
    this.error = null;

    this.notificationsService.createNotification(this.newNotification).subscribe({
      next: () => {
        this.resetForm();
        this.showForm = false;
        this.notificationAdded.emit();
        this.isSubmitting = false;
      },
      error: (err) => {
        this.error = 'Failed to create notification. Please try again.';
        this.isSubmitting = false;
        console.error('Error creating notification:', err);
      }
    });
  }

  resetForm(): void {
    this.newNotification = { message: '' };
    this.error = null;
  }
}
