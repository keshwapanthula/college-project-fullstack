import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpdatesList } from './updates-list';

describe('UpdatesList', () => {
  let component: UpdatesList;
  let fixture: ComponentFixture<UpdatesList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UpdatesList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdatesList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
