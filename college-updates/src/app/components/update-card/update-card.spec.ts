import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpdateCard } from './update-card';

describe('UpdateCard', () => {
  let component: UpdateCard;
  let fixture: ComponentFixture<UpdateCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UpdateCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
